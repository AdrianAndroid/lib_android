// Generated by ProtoGen, Version=2.3.0.277, Culture=neutral, PublicKeyToken=17b3b1f090c3ea48.  DO NOT EDIT!
#pragma warning disable 1591, 0612
#region Designer generated code

using pb = global::Google.ProtocolBuffers;
using pbc = global::Google.ProtocolBuffers.Collections;
using pbd = global::Google.ProtocolBuffers.Descriptors;
using scg = global::System.Collections.Generic;
namespace com.tencent.mars.sample.proto {
  
  namespace Proto {
    
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
    [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
    [global::System.CodeDom.Compiler.GeneratedCodeAttribute("ProtoGen", "2.3.0.277")]
    public static partial class ConversationListResponse {
    
      #region Extension registration
      public static void RegisterAllExtensions(pb::ExtensionRegistry registry) {
      }
      #endregion
      #region Static variables
      #endregion
      #region Extensions
      internal static readonly object Descriptor;
      static ConversationListResponse() {
        Descriptor = null;
      }
      #endregion
      
    }
  }
  #region Messages
  [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
  [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
  [global::System.CodeDom.Compiler.GeneratedCodeAttribute("ProtoGen", "2.3.0.277")]
  public sealed partial class ConversationListResponse : pb::GeneratedMessageLite<ConversationListResponse, ConversationListResponse.Builder> {
    private ConversationListResponse() { }
    private static readonly ConversationListResponse defaultInstance = new ConversationListResponse().MakeReadOnly();
    private static readonly string[] _conversationListResponseFieldNames = new string[] { "list" };
    private static readonly uint[] _conversationListResponseFieldTags = new uint[] { 10 };
    public static ConversationListResponse DefaultInstance {
      get { return defaultInstance; }
    }
    
    public override ConversationListResponse DefaultInstanceForType {
      get { return DefaultInstance; }
    }
    
    protected override ConversationListResponse ThisMessage {
      get { return this; }
    }
    
    public const int ListFieldNumber = 1;
    private pbc::PopsicleList<global::com.tencent.mars.sample.proto.Conversation> list_ = new pbc::PopsicleList<global::com.tencent.mars.sample.proto.Conversation>();
    public scg::IList<global::com.tencent.mars.sample.proto.Conversation> ListList {
      get { return list_; }
    }
    public int ListCount {
      get { return list_.Count; }
    }
    public global::com.tencent.mars.sample.proto.Conversation GetList(int index) {
      return list_[index];
    }
    
    public override bool IsInitialized {
      get {
        foreach (global::com.tencent.mars.sample.proto.Conversation element in ListList) {
          if (!element.IsInitialized) return false;
        }
        return true;
      }
    }
    
    public override void WriteTo(pb::ICodedOutputStream output) {
      int size = SerializedSize;
      string[] field_names = _conversationListResponseFieldNames;
      if (list_.Count > 0) {
        output.WriteMessageArray(1, field_names[0], list_);
      }
    }
    
    private int memoizedSerializedSize = -1;
    public override int SerializedSize {
      get {
        int size = memoizedSerializedSize;
        if (size != -1) return size;
        
        size = 0;
        foreach (global::com.tencent.mars.sample.proto.Conversation element in ListList) {
          size += pb::CodedOutputStream.ComputeMessageSize(1, element);
        }
        memoizedSerializedSize = size;
        return size;
      }
    }
    
    #region Lite runtime methods
    public override int GetHashCode() {
      int hash = GetType().GetHashCode();
      foreach(global::com.tencent.mars.sample.proto.Conversation i in list_)
        hash ^= i.GetHashCode();
      return hash;
    }
    
    public override bool Equals(object obj) {
      ConversationListResponse other = obj as ConversationListResponse;
      if (other == null) return false;
      if(list_.Count != other.list_.Count) return false;
      for(int ix=0; ix < list_.Count; ix++)
        if(!list_[ix].Equals(other.list_[ix])) return false;
      return true;
    }
    
    public override void PrintTo(global::System.IO.TextWriter writer) {
      PrintField("list", list_, writer);
    }
    #endregion
    
    public static ConversationListResponse ParseFrom(byte[] data) {
      return ((Builder) CreateBuilder().MergeFrom(data)).BuildParsed();
    }
    private ConversationListResponse MakeReadOnly() {
      list_.MakeReadOnly();
      return this;
    }
    
    public static Builder CreateBuilder() { return new Builder(); }
    public override Builder ToBuilder() { return CreateBuilder(this); }
    public override Builder CreateBuilderForType() { return new Builder(); }
    public static Builder CreateBuilder(ConversationListResponse prototype) {
      return new Builder(prototype);
    }
    
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
    [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
    [global::System.CodeDom.Compiler.GeneratedCodeAttribute("ProtoGen", "2.3.0.277")]
    public sealed partial class Builder : pb::GeneratedBuilderLite<ConversationListResponse, Builder> {
      protected override Builder ThisBuilder {
        get { return this; }
      }
      public Builder() {
        result = DefaultInstance;
        resultIsReadOnly = true;
      }
      internal Builder(ConversationListResponse cloneFrom) {
        result = cloneFrom;
        resultIsReadOnly = true;
      }
      
      private bool resultIsReadOnly;
      private ConversationListResponse result;
      
      private ConversationListResponse PrepareBuilder() {
        if (resultIsReadOnly) {
          ConversationListResponse original = result;
          result = new ConversationListResponse();
          resultIsReadOnly = false;
          MergeFrom(original);
        }
        return result;
      }
      
      public override bool IsInitialized {
        get { return result.IsInitialized; }
      }
      
      protected override ConversationListResponse MessageBeingBuilt {
        get { return PrepareBuilder(); }
      }
      
      public override Builder Clear() {
        result = DefaultInstance;
        resultIsReadOnly = true;
        return this;
      }
      
      public override Builder Clone() {
        if (resultIsReadOnly) {
          return new Builder(result);
        } else {
          return new Builder().MergeFrom(result);
        }
      }
      
      public override ConversationListResponse DefaultInstanceForType {
        get { return global::com.tencent.mars.sample.proto.ConversationListResponse.DefaultInstance; }
      }
      
      public override ConversationListResponse BuildPartial() {
        if (resultIsReadOnly) {
          return result;
        }
        resultIsReadOnly = true;
        return result.MakeReadOnly();
      }
      
      public override Builder MergeFrom(pb::IMessageLite other) {
        if (other is ConversationListResponse) {
          return MergeFrom((ConversationListResponse) other);
        } else {
          base.MergeFrom(other);
          return this;
        }
      }
      
      public override Builder MergeFrom(ConversationListResponse other) {
      return this;
      }
      
      public override Builder MergeFrom(pb::ICodedInputStream input) {
        return MergeFrom(input, pb::ExtensionRegistry.Empty);
      }
      
      public override Builder MergeFrom(pb::ICodedInputStream input, pb::ExtensionRegistry extensionRegistry) {
        PrepareBuilder();
        uint tag;
        string field_name;
        while (input.ReadTag(out tag, out field_name)) {
          if(tag == 0 && field_name != null) {
            int field_ordinal = global::System.Array.BinarySearch(_conversationListResponseFieldNames, field_name, global::System.StringComparer.Ordinal);
            if(field_ordinal >= 0)
              tag = _conversationListResponseFieldTags[field_ordinal];
            else {
              ParseUnknownField(input, extensionRegistry, tag, field_name);
              continue;
            }
          }
          switch (tag) {
            case 0: {
              throw pb::InvalidProtocolBufferException.InvalidTag();
            }
            default: {
              if (pb::WireFormat.IsEndGroupTag(tag)) {
                return this;
              }
              ParseUnknownField(input, extensionRegistry, tag, field_name);
              break;
            }
            case 10: {
              input.ReadMessageArray(tag, field_name, result.list_, global::com.tencent.mars.sample.proto.Conversation.DefaultInstance, extensionRegistry);
              break;
            }
          }
        }
        
        return this;
        }
        
        
        public pbc::IPopsicleList<global::com.tencent.mars.sample.proto.Conversation> ListList {
          get { return PrepareBuilder().list_; }
        }
        public int ListCount {
          get { return result.ListCount; }
        }
        public global::com.tencent.mars.sample.proto.Conversation GetList(int index) {
          return result.GetList(index);
        }
        public Builder SetList(int index, global::com.tencent.mars.sample.proto.Conversation value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.list_[index] = value;
          return this;
        }
        public Builder SetList(int index, global::com.tencent.mars.sample.proto.Conversation.Builder builderForValue) {
          pb::ThrowHelper.ThrowIfNull(builderForValue, "builderForValue");
          PrepareBuilder();
          result.list_[index] = builderForValue.Build();
          return this;
        }
        public Builder AddList(global::com.tencent.mars.sample.proto.Conversation value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.list_.Add(value);
          return this;
        }
        public Builder AddList(global::com.tencent.mars.sample.proto.Conversation.Builder builderForValue) {
          pb::ThrowHelper.ThrowIfNull(builderForValue, "builderForValue");
          PrepareBuilder();
          result.list_.Add(builderForValue.Build());
          return this;
        }
        public Builder AddRangeList(scg::IEnumerable<global::com.tencent.mars.sample.proto.Conversation> values) {
          PrepareBuilder();
          result.list_.Add(values);
          return this;
        }
        public Builder ClearList() {
          PrepareBuilder();
          result.list_.Clear();
          return this;
        }
      }
      static ConversationListResponse() {
        object.ReferenceEquals(global::com.tencent.mars.sample.proto.Proto.ConversationListResponse.Descriptor, null);
      }
    }
    
    #endregion
    
  }
  
  #endregion Designer generated code
